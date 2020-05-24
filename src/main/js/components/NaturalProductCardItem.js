import Card from "react-bootstrap/Card";
import Table from "react-bootstrap/Table";
import Utils from "../Utils";

const React = require("react");


//This is the small cards that appear on the home page - need to find what catches the search

export default class NaturalProductCardItem extends React.Component {
    render() {
        const linkToCompoundPage = "/compound/coconut_id/" + this.props.naturalProduct.coconut_id;
        const structure = Utils.drawMoleculeBySmiles(this.props.naturalProduct.smiles);
        //console.log(this.props.naturalProduct.smiles);
        // console.log(this.props.naturalProduct.coconut_id)

        return (
            <Card className="cardBrowserItem">
                <Card.Img variant="top" src={structure.toDataURL()} alt="🥥"/>
                <Card.Body>
                    <Card.Title>
                        <Card.Link href={linkToCompoundPage} className="cardItemHeadline">{this.props.naturalProduct.coconut_id}</Card.Link>
                    </Card.Title>
                    <Card.Subtitle>{this.props.naturalProduct.name ? this.props.naturalProduct.name : "no name available"}</Card.Subtitle>
                    <Table>
                        <tbody>
                        <tr>
                            <td>Mol. formula</td>
                            <td>{this.props.naturalProduct.molecular_formula || this.props.naturalProduct.molecularFormula}</td>
                        </tr>
                        <tr>
                            <td>Mol. weight</td>
                            <td>{this.props.naturalProduct.molecular_weight || this.props.naturalProduct.molecularWeight}</td>
                        </tr>
                        <tr>
                            <td>NPL score</td>
                            <td>{this.props.naturalProduct.npl_score}</td>
                        </tr>
                        </tbody>
                    </Table>
                </Card.Body>
            </Card>
        );
    }
}